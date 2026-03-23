// 웹 프론트에서 공통 클래스 조합에 사용하는 유틸 파일이다.
export function cn(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(" ");
}
