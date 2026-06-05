CREATE SCHEMA IF NOT EXISTS map_view;

CREATE TABLE IF NOT EXISTS map_view.marker_layers (
    code           VARCHAR(50)  PRIMARY KEY,
    label_ko       VARCHAR(100) NOT NULL,
    label_en       VARCHAR(100) NOT NULL,
    icon_key       VARCHAR(100),
    display_order  INT          NOT NULL DEFAULT 0,
    default_visible BOOLEAN     NOT NULL DEFAULT TRUE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS map_view.markers (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    layer_code          VARCHAR(50) NOT NULL REFERENCES map_view.marker_layers(code),
    place_ref           UUID,
    label               VARCHAR(200) NOT NULL,
    lat                 DOUBLE PRECISION NOT NULL,
    lng                 DOUBLE PRECISION NOT NULL,
    geohash             VARCHAR(12) NOT NULL,
    icon_key            VARCHAR(100),
    visibility          VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    filter_json         JSONB,
    published_revision  INT         NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_markers_layer_code  ON map_view.markers(layer_code);
CREATE INDEX IF NOT EXISTS idx_markers_place_ref   ON map_view.markers(place_ref);
CREATE INDEX IF NOT EXISTS idx_markers_geohash     ON map_view.markers(geohash);
CREATE INDEX IF NOT EXISTS idx_markers_visibility  ON map_view.markers(visibility);

CREATE TABLE IF NOT EXISTS map_view.marker_publication_events (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    marker_id   UUID        NOT NULL REFERENCES map_view.markers(id),
    event_type  VARCHAR(30) NOT NULL,
    payload_json JSONB,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pub_events_marker_id   ON map_view.marker_publication_events(marker_id);
CREATE INDEX IF NOT EXISTS idx_pub_events_consumed_at ON map_view.marker_publication_events(consumed_at);
