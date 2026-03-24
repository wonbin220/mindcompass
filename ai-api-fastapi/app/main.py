# ai-api 애플리케이션과 내부 라우터를 구성하는 시작 파일입니다.
from fastapi import FastAPI

from app.routers.chat_router import router as chat_router
from app.routers.diary_router import router as diary_router
from app.routers.safety_router import router as safety_router

app = FastAPI()
app.include_router(diary_router)
app.include_router(chat_router)
app.include_router(safety_router)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/test/hello")
def test_hello():
    return {
        "message": "hello from fastapi",
        "service": "ai-api"
    }
