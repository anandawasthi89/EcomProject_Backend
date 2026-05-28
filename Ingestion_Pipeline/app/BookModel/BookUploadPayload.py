from pydantic import BaseModel

class BookUploadPayload(BaseModel):
    eventId:str
    title:str
    author:str
    publisher:str
    publishedDate:str
    language:str
    pageCount:int