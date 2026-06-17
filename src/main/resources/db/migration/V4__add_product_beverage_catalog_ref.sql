-- Reference string into recommendation-service's beverage catalog (e.g. "whiskey.buffalo_trace_bourbon").
-- admin does not own/replicate the catalog; this is not a foreign key, just an opaque reference.
ALTER TABLE admin.products ADD COLUMN IF NOT EXISTS beverage_catalog_ref TEXT;
