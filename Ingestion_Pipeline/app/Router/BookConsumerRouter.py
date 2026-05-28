from app.BookModel.BookUploadPayload import BookUploadPayload
from app.BookConsumerService.dummyIngest import getDummyIngest
from fastapi import APIRouter

router = APIRouter()

@router.get("/")
def is_alive():
    return {"message": "service is alive!"}

@router.get("/dummyIngest")
def dummy_ingest() -> BookUploadPayload:
    return getDummyIngest()