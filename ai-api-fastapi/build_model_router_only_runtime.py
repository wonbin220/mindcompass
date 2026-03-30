import sys
from fastapi import FastAPI
import uvicorn
sys.path.insert(0, r"C:\programing\mindcompass\ai-api-fastapi")
from app.routers.model_router import router
app = FastAPI()
app.include_router(router)
app.add_api_route('/health', lambda: {'status': 'ok'}, methods=['GET'])
uvicorn.run(app, host='127.0.0.1', port=8002)
