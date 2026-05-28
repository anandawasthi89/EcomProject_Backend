from app.BookConsumerService.BookDataConsumer import lifespan
from app.Router.BookConsumerRouter import router
from fastapi import FastAPI

app = FastAPI(lifespan=lifespan)
app.include_router(router)