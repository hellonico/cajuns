#!/bin/bash

# === Config ===
BASE_URL="https://github.com/hellonico/cajuns/tree/main"  # Change if needed

cd WEB || exit 1

# Start index.html
cat > index.html <<EOF
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Code Samples</title>
  <style>
    body {
      font-family: sans-serif;
      margin: 0;
      padding: 2rem;
      background-color: #f5f5f5;
    }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 2rem;
    }
    .example {
      background: white;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 4px 10px rgba(0,0,0,0.1);
      display: flex;
      flex-direction: column;
    }
    .iframe-container {
      position: relative;
      width: 100%;
      aspect-ratio: 16/9;
      overflow: hidden;
    }
    iframe {
      width: 100%;
      height: 100%;
      border: none;
      pointer-events: none;
      border-radius: 12px 12px 0 0;
    }
    .overlay-link {
      position: absolute;
      inset: 0;
      z-index: 10;
      cursor: pointer;
    }
    .info {
      padding: 1rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }
    .title {
      font-weight: bold;
      color: #333;
      text-align: center;
      word-break: break-word;
    }
    .src-link {
      font-size: 0.9rem;
      color: #0066cc;
      text-decoration: none;
    }
    .src-link:hover {
      text-decoration: underline;
    }

    .title-line {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
      justify-content: center;
    }

    .chapter-badge {
      background-color: #333;
      color: white;
      font-weight: bold;
      border-radius: 8px;
      padding: 0.2em 0.6em;
      min-width: 2em;
      text-align: center;
    }

    .title-text {
      font-size: 1.1rem;
      font-weight: 600;
      color: #444;
      word-break: break-word;
      text-align: center;
    }

  </style>
</head>
<body>
  <div class="grid">
EOF

# Loop through examples
for dir in [0-9][0-9]-*/; do
  name="${dir%/}"
  chapter="${name%%-*}"                  # Extract "01"
  title="${name#*-}"                     # Extract "pong-game"
  encoded_name=$(echo -n "$name" | jq -sRr @uri)

  cat >> index.html <<EOF
    <div class="example">
      <div class="iframe-container">
        <iframe src="${name}/index.html"></iframe>
        <a class="overlay-link" href="${name}/index.html"></a>
      </div>
      <div class="info">
        <div class="title-line">
          <span class="chapter-badge">${chapter}</span>
          <span class="title-text">${title}</span>
        </div>
        <a class="src-link" href="${BASE_URL}/${encoded_name}" target="_blank">Source</a>
      </div>
    </div>
EOF
done


# Close HTML
cat >> index.html <<EOF
  </div>
</body>
</html>
EOF

echo "Responsive index.html generated in WEB/"
