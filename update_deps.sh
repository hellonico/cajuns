# Usage: find_and_replace <filename> <sed_substitution1> <sed_substitution2> ...
# Example: find_and_replace "package.json" 's/"react":[[:space:]]*"[^"]*"/"react": "^18.3.1"/g' 's/"react-dom":[[:space:]]*"[^"]*"/"react-dom": "^18.3.1"/g'
find_and_replace() {
    local filename="$1"
    shift  # Remove filename from arguments, leaving only sed patterns

    # Build the sed command with all -e expressions
    local sed_command=()
    for pattern in "$@"; do
        sed_command+=("-e" "$pattern")
    done

    # Execute find with sed
    find . -name "$filename" -exec sed -i '' "${sed_command[@]}" {} \;
}

find_and_replace "package.json" \
  's/"react":[[:space:]]*"[^"]*"/"react": "^18.3.1"/g' \
  's/"react-dom":[[:space:]]*"[^"]*"/"react-dom": "^18.3.1"/g' \
  's/"shadow-cljs":[[:space:]]*"[^"]*"/"shadow-cljs": "3.1.5"/g' \

find_and_replace "deps.edn" \
  's/org\.clojure\/clojurescript {:mvn\/version "1\.[^"]*"}/org.clojure\/clojurescript {:mvn\/version "1.12.42"}/g' \

find_and_replace "shadow-cljs.edn" \
  's/reagent\/reagent "1\.[^"]*"/reagent\/reagent "1.3.0"/g' \
