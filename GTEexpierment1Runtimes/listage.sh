find . -type f -printf '%T@ %p\n' |
sort -nr |
while read ts path; do
  age=$(( $(date +%s) - ${ts%.*} ))
  # pick a nice unit
  if   (( age < 60 ));      then fmt="${age}s";
  elif (( age < 3600 ));    then fmt="$((age/60))m";
  elif (( age < 86400 ));   then fmt="$((age/3600))h";
  else                           fmt="$((age/86400))d";
  fi
  printf '%-6s %s\n' "$fmt" "$path"
done
