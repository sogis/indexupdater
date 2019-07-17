curl -X POST -H 'Content-Type: application/json' 'http://localhost:8983/solr/gdi/update?commitWithin=1000' --data-binary '
{
	"delete": {
		"query": "facet:fill_0_10k"
	}
}
'
