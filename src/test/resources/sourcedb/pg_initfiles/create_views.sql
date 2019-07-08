CREATE OR REPLACE VIEW public.solr_index_fill_0_10k_v AS
WITH 
base AS (
    SELECT 
        'ch.so.agi.fill_0_10k'::text AS subclass,
        generate_series.generate_series::text AS id_in_class,
        'fill lorem ipsum solr rocks:'::text || generate_series.generate_series::text AS displaytext,
        (((random()::text || ' | '::text) || generate_series.generate_series) || ' | '::text) || random()::text AS part_1
    FROM generate_series(0, 10000) generate_series(generate_series)
)

SELECT
    (array_to_json(array_append(ARRAY[subclass::text], id_in_class::text)))::text AS id,
    displaytext AS display,
    part_1 AS search_1_stem,
    part_1 AS search_2_stem,
    part_1 AS sort,
    subclass AS facet,
    (array_to_json(array_append(ARRAY['none'::text], 'str:y'::text)))::text AS idfield_meta
FROM
    base
;


CREATE OR REPLACE VIEW public.solr_index_fill_10k_60k_v AS
WITH 
base AS (
    SELECT 
        'ch.so.agi.fill_10k_60k'::text AS subclass,
        generate_series.generate_series::text AS id_in_class,
        'fill lorem ipsum solr rocks:'::text || generate_series.generate_series::text AS displaytext,
        (((random()::text || ' | '::text) || generate_series.generate_series) || ' | '::text) || random()::text AS part_1
    FROM generate_series(10001, 60000) generate_series(generate_series)
)

SELECT
    (array_to_json(array_append(ARRAY[subclass::text], id_in_class::text)))::text AS id,
    displaytext AS display,
    part_1 AS search_1_stem,
    part_1 AS search_2_stem,
    part_1 AS sort,
    subclass AS facet,
    (array_to_json(array_append(ARRAY['none'::text], 'str:y'::text)))::text AS idfield_meta
FROM
    base
;
