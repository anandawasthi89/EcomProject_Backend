import asyncio
import fitz
import re
from pathlib import Path
from app.Engine.BookExtractor import BookExtractor
from app.Engine.EmbeddingService import EmbeddingService
from app.Engine.QDrantStore import QDrantStore

async def documentIngestion(path:str, book_name:str):
    print(f"in documentIngestion method <<")
    print(f"path: {path}")
    return await asyncio.to_thread(documentIngestionThread, path, book_name)

def documentIngestionThread(path:str, book_name:str):
    print(f"in documentIngestionThread method <<")
    print(f"path: {path} book_name: {book_name}")
    folder_path = Path(path)
    full_path = folder_path / book_name
    bookExtractor = BookExtractor(book_name=book_name)
    embeddingService = EmbeddingService("sentence-transformers/all-MiniLM-L6-v2")
    qDrantStore = QDrantStore(book_name,embeddingService.embeddings_model)
    doc = fitz.open(full_path)
    try:
        print(f"doc.page_count: {doc.page_count}")
        for page_num in range(len(doc)):
            page = doc.load_page(page_num)
            text = normalize_text(page.get_text())
            chunks_metadata = bookExtractor.add_text_buffer_metadata(text=text)
            print(f"sending {len(chunks_metadata)} chunks for embedding")
            if len(chunks_metadata) != 0:
                for chunk_metadata in chunks_metadata:
                    qDrantStore.add_documents(chunk_data=chunk_metadata)
            if len(qDrantStore.documents)>=100:
                print("over 100 docs")
                qDrantStore.update_collection()
                qDrantStore.flush_documents()
                print("flushed")
        chunks_metadata = bookExtractor.return_remaining_buffer_metadata()
        print(f"sending {len(chunks_metadata)} chunks for embedding")
        if len(chunks_metadata) != 0:
            for chunk_metadata in chunks_metadata:
                qDrantStore.add_documents(chunk_data=chunk_metadata)
        qDrantStore.update_collection()
        qDrantStore.flush_documents()
        print("ingestion done. now question")

        query = "who is Amy March?"
        qDrantStore.get_results(query=query)

    finally:
        qDrantStore.close()
        doc.close()

    return True

def normalize_text(text: str) -> str:
    """
    Cleans and standardizes raw document/page text immediately 
    before it is pushed into a token-counting rotating buffer.
    """
    if not text:
        return ""

    # 1. Stitch words broken across line wraps (e.g., "sys-\ntem" -> "system")
    text = text.replace("-\n", "").replace("-\r\n", "")

    # 2. Standardize all line breaks to standard Unix format
    text = text.replace("\r\n", "\n")

    # 3. Shield intentional paragraphs/list blocks (2 or more newlines)
    # Using a rare string sequence to prevent regex from destroying layout
    text = re.sub(r"\n{2,}", " _PARAGRAPH_BREAK_ ", text)

    # 4. Turn single line wraps (mid-sentence breaks) into standard spaces
    text = text.replace("\n", " ")

    # 5. Compress multiple consecutive spaces or tabs into a single space
    text = re.sub(r"[ \t]+", " ", text)

    # 6. Restore the intentional structural paragraph breaks as clean double newlines
    text = text.replace(" _PARAGRAPH_BREAK_ ", "\n\n")

    # 7. Strip trailing/leading space from individual lines to keep tokens dense
    text = "\n".join(line.strip() for line in text.split("\n"))

    return text.strip()