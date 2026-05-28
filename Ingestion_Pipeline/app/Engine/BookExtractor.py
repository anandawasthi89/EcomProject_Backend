from langchain_text_splitters import RecursiveCharacterTextSplitter

class BookExtractor:

    def __init__(
        self,
        book_name: str,
        chunk_size=1000,
        chunk_overlap=250,
        buffer_threshold=12000
    ):
        self.book_name = book_name
        self.text_buffer = ""
        self.buffer_threshold = buffer_threshold
        self.idx = 1

        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap
        )

    def add_text_buffer_metadata(self, text: str):

        self.text_buffer += " " + text

        if len(self.text_buffer) > self.buffer_threshold:

            chunks = self.create_chunks(self.text_buffer)

            # keep final chunk as carryover buffer
            self.text_buffer = chunks[-1]

            # return finalized chunks only
            return self.chunks_to_buffer_metadata(chunks[:-1])

        return []

    def return_remaining_buffer_metadata(self):

        if not self.text_buffer.strip():
            return []

        chunks = self.create_chunks(self.text_buffer)

        self.text_buffer = ""

        return self.chunks_to_buffer_metadata(chunks)

    def create_chunks(self, text: str):

        return self.splitter.split_text(text)
    
    def chunks_to_buffer_metadata(self, chunks):
        buffer_metadata = []
        for chunk in chunks:
            buffer_metadata.append(
                {
                "book": self.book_name,
                "chunk_index": self.idx,
                "text": chunk
                }
            )
            self.idx+=1
        return buffer_metadata