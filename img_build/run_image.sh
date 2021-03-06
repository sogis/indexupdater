#!/bin/bash

export SPRING_APPLICATION_JSON='{"solrProtocol":"http","solrHost":"localhost","solrPort":8983,"solrPathQuery":"solr/gdi/select","solrPathUpdate":"solr/gdi/update","logSilenceMaxDurationSeconds":5,"dihPollIntervalSeconds":2,"dihImportMaxDurationSeconds":120,"dihDefaultPath":"solr/gdi/dih","logLevel":"debug"}'

if [ "$1" == "bg" ] #bg - background
  then
    PARA="-d"
  else
    PARA="-it"
fi

docker run $PARA \
    --name indexupdater \
    -e "SPRING_APPLICATION_JSON" \
    --network host \
    --rm \
    sogis/indexupdater
