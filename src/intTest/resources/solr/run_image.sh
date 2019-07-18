# The following run command starts a new container with the configuration for the index "gdi". 
# The indexed data will NOT be placed on a persistent volume --> Must be reindexed when a container is (re)started


docker run -d \
	--name solr \
	--network=host \
	-P -d \
	-v $PWD/configsets/gdi:/gdi_conf \
	solr:7.7.2 \
	solr-precreate gdi /gdi_conf

# alternative solr command to add the core, see official image documentaton: solr-create -c gdi -d /gdi_conf



