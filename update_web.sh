#!/bin/bash

# Create the WEB directory if it doesn't exist
mkdir -p WEB

# Loop through all folders starting with a number and dash
for dir in [0-9][0-9]-*/; do
  echo "Processing $dir..."

  # Run shadow-cljs inside the folder
  (cd "$dir" && npx shadow-cljs release app)

  # Prepare the destination folder
  dest="WEB/${dir%/}"  # Remove trailing slash
  mkdir -p "$dest"

  # Copy contents of public folder into the destination (not the folder itself)
  cp -r "${dir}public/"* "$dest"
done

echo "All done!"
