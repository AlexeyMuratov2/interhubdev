-- =============================================================================
-- Building as separate entity; room references building by FK
-- =============================================================================

CREATE TABLE building (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE building IS 'Building (campus block). Rooms belong to a building.';

-- Create one building per distinct room.building name (for existing data)
INSERT INTO building (id, name, created_at, updated_at)
SELECT gen_random_uuid(), d.building, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT DISTINCT building AS building FROM room) d;

-- Add FK column (nullable until we backfill)
ALTER TABLE room ADD COLUMN building_id UUID REFERENCES building(id);

-- Link each room to the building with matching name
UPDATE room r
SET building_id = (SELECT b.id FROM building b WHERE b.name = r.building LIMIT 1);

ALTER TABLE room ALTER COLUMN building_id SET NOT NULL;
ALTER TABLE room DROP COLUMN building;

CREATE INDEX idx_room_building_id ON room(building_id);
COMMENT ON COLUMN room.building_id IS 'Building (campus block) this room belongs to.';
