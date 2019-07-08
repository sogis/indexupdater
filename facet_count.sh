curl -v -G \
--data-urlencode "omitHeader=true" \
--data-urlencode "q=facet:ch.so.agi.fill_0_10k" \
--data-urlencode "rows=1" \
--data-urlencode "fl=display" \
--data-urlencode "omitHeader=true" \
--data-urlencode "facet=true" \
--data-urlencode "facet.field=facet" \
http://localhost:8983/solr/gdi/select

















