# Base image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Copy dependencies first (for caching)
COPY requirements.txt .

# Install dependencies
RUN pip install --upgrade pip

RUN pip install torch --index-url https://download.pytorch.org/whl/cpu

RUN pip install \
    --no-cache-dir \
    --default-timeout=1000 \
    -r requirements.txt

# Copy project
COPY . .

# Expose port
EXPOSE 9006

# Run app
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9006"]