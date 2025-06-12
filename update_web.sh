#!/bin/bash

# Get optional folder argument
TARGET_FOLDER="$1"

# Create the WEB directory if it doesn't exist
mkdir -p WEB

# Loop through all folders like 01-*, 02-* etc.
for dir in [0-9][0-9]-*/; do
  dir_name="${dir%/}"  # Remove trailing slash

  # If a folder was passed as an argument, skip others
  if [[ -n "$TARGET_FOLDER" && "$dir_name" != "$TARGET_FOLDER" ]]; then
    continue
  fi

  echo "Processing $dir_name..."

  # Run build command inside the folder
  (cd "$dir_name" && npx shadow-cljs release app)

  # Prepare the destination
  dest="WEB/$dir_name"
  mkdir -p "$dest"

  # Copy contents of public folder, not the folder itself
  cp -r "$dir_name/public/"* "$dest"
done

echo "Done."
