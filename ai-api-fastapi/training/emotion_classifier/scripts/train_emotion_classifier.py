# KcELECTRA 감정분류 모델을 파인튜닝하는 학습 스크립트 스켈레톤입니다.
# 감정 분류 학습 실험용 스크립트입니다.
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

import numpy as np
import pandas as pd
import torch
from sklearn.metrics import accuracy_score, f1_score
from transformers import (
    AutoModelForSequenceClassification,
    AutoTokenizer,
    DataCollatorWithPadding,
    EarlyStoppingCallback,
    Trainer,
    TrainingArguments
)


class EmotionDataset:
    def __init__(self, dataframe: pd.DataFrame, tokenizer, label_to_id, max_length: int) -> None:
        self.texts = dataframe["text"].tolist()
        self.labels = [label_to_id[label] for label in dataframe["service_label"].tolist()]
        self.tokenizer = tokenizer
        self.max_length = max_length

    def __len__(self) -> int:
        return len(self.texts)

    def __getitem__(self, index: int):
        encoded = self.tokenizer(
            self.texts[index],
            truncation=True,
            max_length=self.max_length
        )
        encoded["labels"] = self.labels[index]
        return encoded


class WeightedTrainer(Trainer):
    def __init__(
        self,
        *args,
        class_weights: torch.Tensor | None = None,
        happy_id: int | None = None,
        calm_id: int | None = None,
        happy_to_calm_penalty_weight: float = 0.0,
        happy_calm_bidirectional_penalty_weight: float = 0.0,
        **kwargs
    ) -> None:
        super().__init__(*args, **kwargs)
        self.class_weights = class_weights
        self.happy_id = happy_id
        self.calm_id = calm_id
        self.happy_to_calm_penalty_weight = happy_to_calm_penalty_weight
        self.happy_calm_bidirectional_penalty_weight = happy_calm_bidirectional_penalty_weight

    def compute_loss(self, model, inputs, return_outputs=False):
        labels = inputs.pop("labels")
        outputs = model(**inputs)
        logits = outputs.get("logits")
        label_smoothing = getattr(self.args, "label_smoothing_factor", 0.0)

        if self.class_weights is None:
            loss_fct = torch.nn.CrossEntropyLoss(label_smoothing=label_smoothing)
        else:
            loss_fct = torch.nn.CrossEntropyLoss(
                weight=self.class_weights.to(logits.device),
                label_smoothing=label_smoothing
            )

        loss = loss_fct(logits.view(-1, model.config.num_labels), labels.view(-1))

        if (
            self.happy_to_calm_penalty_weight > 0.0
            and self.happy_id is not None
            and self.calm_id is not None
        ):
            happy_mask = labels == self.happy_id
            if torch.any(happy_mask):
                happy_logits = logits[happy_mask]
                happy_probabilities = torch.softmax(happy_logits, dim=-1)
                happy_to_calm_penalty = happy_probabilities[:, self.calm_id].mean()
                loss = loss + (self.happy_to_calm_penalty_weight * happy_to_calm_penalty)

        # HAPPY/CALM 경계를 한쪽으로 밀지 않도록 양방향 확률을 함께 억제한다.
        if (
            self.happy_calm_bidirectional_penalty_weight > 0.0
            and self.happy_id is not None
            and self.calm_id is not None
        ):
            directional_penalties = []

            happy_mask = labels == self.happy_id
            if torch.any(happy_mask):
                happy_logits = logits[happy_mask]
                happy_probabilities = torch.softmax(happy_logits, dim=-1)
                directional_penalties.append(happy_probabilities[:, self.calm_id].mean())

            calm_mask = labels == self.calm_id
            if torch.any(calm_mask):
                calm_logits = logits[calm_mask]
                calm_probabilities = torch.softmax(calm_logits, dim=-1)
                directional_penalties.append(calm_probabilities[:, self.happy_id].mean())

            if directional_penalties:
                bidirectional_penalty = torch.stack(directional_penalties).mean()
                loss = loss + (
                    self.happy_calm_bidirectional_penalty_weight
                    * bidirectional_penalty
                )

        return (loss, outputs) if return_outputs else loss


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--train-csv", required=True)
    parser.add_argument("--valid-csv", required=True)
    parser.add_argument("--config", required=True)
    parser.add_argument("--label-map", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--resume-from-checkpoint")
    parser.add_argument("--auto-resume-last-checkpoint", action="store_true")
    return parser.parse_args()


def load_json(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as file:
        return json.load(file)


def get_active_labels(
    train_df: pd.DataFrame,
    valid_df: pd.DataFrame,
    full_label_to_id: dict[str, int]
) -> list[str]:
    full_label_order = [
        label
        for label, _ in sorted(full_label_to_id.items(), key=lambda item: item[1])
    ]
    active_label_set = set(train_df["service_label"].tolist()) | set(valid_df["service_label"].tolist())
    return [label for label in full_label_order if label in active_label_set]


def build_compute_metrics(id_to_label: dict[int, str]):
    happy_id = next((label_id for label_id, label in id_to_label.items() if label == "HAPPY"), None)
    calm_id = next((label_id for label_id, label in id_to_label.items() if label == "CALM"), None)

    def compute_metrics(eval_pred):
        logits, labels = eval_pred
        predictions = np.argmax(logits, axis=-1)
        metrics = {
            "accuracy": accuracy_score(labels, predictions),
            "macro_f1": f1_score(labels, predictions, average="macro"),
            "weighted_f1": f1_score(labels, predictions, average="weighted")
        }

        if happy_id is not None and calm_id is not None:
            happy_mask = labels == happy_id
            calm_mask = labels == calm_id
            happy_support = int(np.sum(happy_mask))
            calm_support = int(np.sum(calm_mask))
            happy_to_calm_count = int(np.sum(predictions[happy_mask] == calm_id))
            calm_to_happy_count = int(np.sum(predictions[calm_mask] == happy_id))
            happy_calm_mask = np.isin(labels, [happy_id, calm_id])

            metrics["happy_calm_macro_f1"] = (
                f1_score(
                    labels[happy_calm_mask],
                    predictions[happy_calm_mask],
                    labels=[happy_id, calm_id],
                    average="macro"
                )
                if np.any(happy_calm_mask)
                else 0.0
            )
            metrics["happy_to_calm_count"] = float(happy_to_calm_count)
            metrics["happy_to_calm_rate"] = (
                happy_to_calm_count / happy_support if happy_support else 0.0
            )
            metrics["calm_to_happy_count"] = float(calm_to_happy_count)
            metrics["calm_to_happy_rate"] = (
                calm_to_happy_count / calm_support if calm_support else 0.0
            )
            happy_guard_score = 1.0 - metrics["happy_to_calm_rate"]
            calm_guard_score = 1.0 - metrics["calm_to_happy_rate"]
            balanced_guard_score = (happy_guard_score + calm_guard_score) / 2.0
            metrics["happy_calm_balanced_guard_score"] = (
                2.0
                * metrics["happy_calm_macro_f1"]
                * balanced_guard_score
                / (metrics["happy_calm_macro_f1"] + balanced_guard_score)
                if (metrics["happy_calm_macro_f1"] + balanced_guard_score) > 0
                else 0.0
            )

        return metrics

    return compute_metrics


def build_class_weights(labels: list[int], num_labels: int) -> torch.Tensor:
    counts = Counter(labels)
    total_count = sum(counts.values())

    weights = []
    for class_id in range(num_labels):
        class_count = counts.get(class_id, 0)
        if class_count == 0:
            weights.append(0.0)
            continue
        weights.append(total_count / (num_labels * class_count))

    return torch.tensor(weights, dtype=torch.float32)


def find_latest_checkpoint(output_dir: Path) -> Path | None:
    checkpoints = []
    for candidate in output_dir.glob("checkpoint-*"):
        if not candidate.is_dir():
            continue
        try:
            step = int(candidate.name.split("-", maxsplit=1)[1])
        except (IndexError, ValueError):
            continue
        checkpoints.append((step, candidate))

    if not checkpoints:
        return None
    return max(checkpoints, key=lambda item: item[0])[1]


def main() -> None:
    args = parse_args()
    config = load_json(args.config)
    label_map = load_json(args.label_map)
    full_label_to_id = label_map["label_to_id"]

    train_df = pd.read_csv(args.train_csv, encoding="utf-8-sig")
    valid_df = pd.read_csv(args.valid_csv, encoding="utf-8-sig")
    active_labels = get_active_labels(train_df, valid_df, full_label_to_id)
    label_to_id = {label: index for index, label in enumerate(active_labels)}
    id_to_label = {index: label for label, index in label_to_id.items()}

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    resume_checkpoint: Path | None = None
    if args.resume_from_checkpoint:
        resume_checkpoint = Path(args.resume_from_checkpoint)
        if not resume_checkpoint.exists():
            raise FileNotFoundError(f"resume checkpoint not found: {resume_checkpoint}")
    elif args.auto_resume_last_checkpoint:
        resume_checkpoint = find_latest_checkpoint(output_dir)

    model_source = str(resume_checkpoint) if resume_checkpoint else config["model_name"]
    tokenizer = AutoTokenizer.from_pretrained(model_source)
    model = AutoModelForSequenceClassification.from_pretrained(
        model_source,
        num_labels=len(active_labels),
        id2label=id_to_label,
        label2id=label_to_id,
        hidden_dropout_prob=config.get("hidden_dropout_prob", 0.3),
        attention_probs_dropout_prob=config.get("attention_probs_dropout_prob", 0.3)
    )

    train_dataset = EmotionDataset(train_df, tokenizer, label_to_id, config["max_length"])
    valid_dataset = EmotionDataset(valid_df, tokenizer, label_to_id, config["max_length"])
    class_weights = build_class_weights(train_dataset.labels, len(active_labels))

    callbacks = []
    early_stopping_patience = config.get("early_stopping_patience")
    if early_stopping_patience is not None:
        callbacks.append(EarlyStoppingCallback(early_stopping_patience=early_stopping_patience))

    training_arguments = TrainingArguments(
        output_dir=str(output_dir),
        num_train_epochs=config["num_train_epochs"],
        learning_rate=config["learning_rate"],
        per_device_train_batch_size=config["train_batch_size"],
        per_device_eval_batch_size=config["eval_batch_size"],
        gradient_accumulation_steps=config.get("gradient_accumulation_steps", 1),
        weight_decay=config["weight_decay"],
        warmup_ratio=config["warmup_ratio"],
        evaluation_strategy="epoch",
        save_strategy="epoch",
        load_best_model_at_end=True,
        metric_for_best_model=config["metric_for_best_model"],
        greater_is_better=config["greater_is_better"],
        logging_steps=config.get("logging_steps", 50),
        save_total_limit=config.get("save_total_limit", 2),
        dataloader_num_workers=config.get("dataloader_num_workers", 0),
        label_smoothing_factor=config.get("label_smoothing", 0.0),
        seed=config["seed"],
        report_to=[]
    )

    trainer = WeightedTrainer(
        model=model,
        args=training_arguments,
        train_dataset=train_dataset,
        eval_dataset=valid_dataset,
        tokenizer=tokenizer,
        data_collator=DataCollatorWithPadding(tokenizer=tokenizer),
        compute_metrics=build_compute_metrics(id_to_label),
        callbacks=callbacks,
        class_weights=class_weights,
        happy_id=label_to_id.get("HAPPY"),
        calm_id=label_to_id.get("CALM"),
        happy_to_calm_penalty_weight=config.get("happy_to_calm_penalty_weight", 0.0),
        happy_calm_bidirectional_penalty_weight=config.get(
            "happy_calm_bidirectional_penalty_weight",
            0.0
        )
    )

    if resume_checkpoint:
        print(f"resume_checkpoint={resume_checkpoint}")

    trainer.train(resume_from_checkpoint=str(resume_checkpoint) if resume_checkpoint else None)
    trainer.save_model(str(output_dir / "best"))
    tokenizer.save_pretrained(str(output_dir / "best"))

    metadata = {
        "model_name": config["model_name"],
        "active_labels": active_labels,
        "num_labels": len(active_labels),
        "train_distribution": dict(Counter(train_df["service_label"].tolist())),
        "valid_distribution": dict(Counter(valid_df["service_label"].tolist()))
    }
    with (output_dir / "best" / "label_metadata.json").open("w", encoding="utf-8") as file:
        json.dump(metadata, file, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
