import os
from contextlib import asynccontextmanager
import redis.asyncio as redis
import asyncio
import json

from fastapi import FastAPI
from app.BookConsumerService.BookIngestion import documentIngestion

from app.BookModel.BookUploadPayload import BookUploadPayload

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
QUEUE_KEY = "Book_Upload_Queue"

redis_client = redis.Redis(
    host=REDIS_HOST,
    port=REDIS_PORT,
    decode_responses=True
)

consumer_task = None

def loadData(data: str):
    book_metadata = BookUploadPayload(
        eventId=data['eventId'],
        title=data['title'],
        author=data['author'],
        publisher=data['publisher'],
        publishedDate=data['publishedDate'],
        language=data['language'],
        pageCount=data['pageCount'])
    return book_metadata

async def consumer():
    print("consuption started")
    while True:
        try:
            item = await redis_client.brpop(QUEUE_KEY)
            _, payload = item
            print(payload)
            data = json.loads(payload)
            book_metdata = loadData(data)
            print(f"\n[CONSUMER] Received: {book_metdata.title}")
            parsed = await documentIngestion("/app/books",book_metdata.title)
            print(f"[CONSUMER] Finished {parsed}\n")
        except asyncio.CancelledError:
            print("[CONSUMER] Cancelled")
            raise
        except Exception as e:
            print(f"[CONSUMER] Error: {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    global consumer_task
    print("python consumer started")
    consumer_task = asyncio.create_task(consumer())
    yield
    consumer_task.cancel()
    try:
        await consumer_task
        await redis_client.close()
    except asyncio.CancelledError:
        pass

    print("[APP] Shutdown complete")
