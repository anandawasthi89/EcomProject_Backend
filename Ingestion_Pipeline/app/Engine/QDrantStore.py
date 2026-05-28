from langchain_qdrant import QdrantVectorStore
from qdrant_client import QdrantClient
from langchain_core.documents import Document
from qdrant_client.http.models import Distance, VectorParams
from uuid import uuid4

class QDrantStore:

    def __init__(self, book_collection_name:str, embeddingService, path="/app/vectorDB"):
        self.client = QdrantClient(path=path)
        if self.client.collection_exists(collection_name=book_collection_name):
            self.vector_store = QdrantVectorStore.from_existing_collection(
                embedding=embeddingService,
                collection_name=book_collection_name,
                path=path  # Replace with your actual storage directory path
            )
        else:
            vector_size = len(
                embeddingService.embed_query("test")
            )
            print(vector_size)
            self.client.create_collection(
                collection_name=book_collection_name,
                vectors_config=VectorParams(size=vector_size, distance=Distance.COSINE),
            )
            self.vector_store = QdrantVectorStore(
                client=self.client,
                collection_name=book_collection_name,
                embedding=embeddingService,
            )
        self.documents = []
    
    def add_documents(self, chunk_data):
        print(f"added embedding with idx id {chunk_data['chunk_index']}")
        chunk_metadata = {
            "book": chunk_data['book'],
            "chunk_index": chunk_data['chunk_index']
        }
        document = Document(
            page_content=chunk_data['text'],
            metadata=chunk_metadata,
        )
        self.documents.append(document)
    
    def update_collection(self):
        print(f"embedding_length: {len(self.documents)}")
        uuids = [str(uuid4()) for _ in range(len(self.documents))]
        self.vector_store.add_documents(documents=self.documents, ids=uuids)

    def flush_documents(self):
        self.documents=[]

    def get_results(self, query:str, k=2):
        results = self.vector_store.similarity_search(
            query, k=k
        )
        for res in results:
            print(f"* {res.page_content} [{res.metadata}]")
    
    def close(self):
        self.client.close()