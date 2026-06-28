# 1. Use an official, lightweight Python runtime
FROM python:3.10-slim

# 2. Set the working directory inside the container
WORKDIR /app

# 3. Install system-level tools needed to compile ML libraries
RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    && rm -rf /var/lib/apt/lists/*

# 4. Copy the requirements file first (This tricks Docker into caching the heavy downloads)
COPY requirements.txt .

# 5. Install the Python libraries
RUN pip install --no-cache-dir -r requirements.txt

# 6. Copy the FastAPI app code into the container
COPY ./app ./app

# 7. Expose port 8000 for the API
EXPOSE 8000

# 8. The command to boot the server
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
