from app.BookModel.BookUploadPayload import BookUploadPayload
import redis
import json

client = redis.Redis(
    host="redis",
    port=6379,
    decode_responses=True
)
QUEUE_KEY = "Book_Upload_Queue"

def getDummyIngest() -> BookUploadPayload:
    _, result = client.blpop(QUEUE_KEY)
    print(result)

    data = json.loads(result)
    print("data: ",data)
    response = BookUploadPayload(
        eventId=data["eventId"],
        title=data["title"],
        author=data["author"],
        publisher=data["publisher"],
        publishedDate=data["publishedDate"],
        language=data["language"],
        pageCount=data["pageCount"]
    )
    print("response: ",response)

    return response