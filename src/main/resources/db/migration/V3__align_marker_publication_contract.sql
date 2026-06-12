-- Align admin marker write model + publication-event OUTBOX with the map-service canonical
-- published projection (see map-service migrations/007_create_map_view_minimal_schema.sql).
-- admin keeps its own DRAFT store (drafts: published_revision = 0, soft-delete via deleted_at);
-- only the publication-event outbox carries the canonical published-only projection payload.

-- 1) markers.place_ref required + one active marker per place (canonical projection key)
UPDATE map_view.markers SET place_ref = gen_random_uuid() WHERE place_ref IS NULL;
ALTER TABLE map_view.markers ALTER COLUMN place_ref SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_markers_place_ref_active
    ON map_view.markers (place_ref) WHERE deleted_at IS NULL;

-- 2) markers.filter_json NOT NULL default {} and must be a JSON object
UPDATE map_view.markers SET filter_json = '{}'::jsonb WHERE filter_json IS NULL;
ALTER TABLE map_view.markers ALTER COLUMN filter_json SET DEFAULT '{}'::jsonb;
ALTER TABLE map_view.markers ALTER COLUMN filter_json SET NOT NULL;
ALTER TABLE map_view.markers
    ADD CONSTRAINT ck_markers_filter_json_object CHECK (jsonb_typeof(filter_json) = 'object');

-- 3) publication-event outbox carries canonical projection keys
ALTER TABLE map_view.marker_publication_events
    ADD COLUMN IF NOT EXISTS place_ref UUID,
    ADD COLUMN IF NOT EXISTS published_revision INT;

UPDATE map_view.marker_publication_events e
   SET place_ref = m.place_ref,
       published_revision = GREATEST(m.published_revision, 1)
  FROM map_view.markers m
 WHERE e.marker_id = m.id AND (e.place_ref IS NULL OR e.published_revision IS NULL);

UPDATE map_view.marker_publication_events SET place_ref = gen_random_uuid() WHERE place_ref IS NULL;
UPDATE map_view.marker_publication_events SET published_revision = 1 WHERE published_revision IS NULL;

ALTER TABLE map_view.marker_publication_events ALTER COLUMN place_ref SET NOT NULL;
ALTER TABLE map_view.marker_publication_events ALTER COLUMN published_revision SET NOT NULL;
ALTER TABLE map_view.marker_publication_events
    ADD CONSTRAINT ck_marker_events_published_revision_positive CHECK (published_revision > 0);

-- 4) marker_id nullable + ON DELETE SET NULL (events survive marker hard-delete)
ALTER TABLE map_view.marker_publication_events
    DROP CONSTRAINT IF EXISTS marker_publication_events_marker_id_fkey;
ALTER TABLE map_view.marker_publication_events ALTER COLUMN marker_id DROP NOT NULL;
ALTER TABLE map_view.marker_publication_events
    ADD CONSTRAINT fk_marker_events_marker_id
    FOREIGN KEY (marker_id) REFERENCES map_view.markers (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_marker_events_place_ref
    ON map_view.marker_publication_events (place_ref);
CREATE INDEX IF NOT EXISTS idx_marker_events_place_revision
    ON map_view.marker_publication_events (place_ref, published_revision);
