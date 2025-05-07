-- Create administrative_regions table
CREATE TABLE IF NOT EXISTS administrative_regions (
                                                      id integer NOT NULL,
                                                      "name" varchar(255) NOT NULL,
    name_en varchar(255) NOT NULL,
    code_name varchar(255) NULL,
    code_name_en varchar(255) NULL,
    CONSTRAINT administrative_regions_pkey PRIMARY KEY (id)
    );

-- Create administrative_units table
CREATE TABLE IF NOT EXISTS administrative_units (
                                                    id integer NOT NULL,
                                                    full_name varchar(255) NULL,
    full_name_en varchar(255) NULL,
    short_name varchar(255) NULL,
    short_name_en varchar(255) NULL,
    code_name varchar(255) NULL,
    code_name_en varchar(255) NULL,
    CONSTRAINT administrative_units_pkey PRIMARY KEY (id)
    );

-- Create temporary tables to handle the migration
CREATE TABLE IF NOT EXISTS provinces_new (
                                             code varchar(20) NOT NULL,
    "name" varchar(255) NOT NULL,
    name_en varchar(255) NULL,
    full_name varchar(255) NOT NULL,
    full_name_en varchar(255) NULL,
    code_name varchar(255) NULL,
    administrative_unit_id integer NULL,
    administrative_region_id integer NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT provinces_new_pkey PRIMARY KEY (code)
    );

CREATE TABLE IF NOT EXISTS districts_new (
                                             code varchar(20) NOT NULL,
    "name" varchar(255) NOT NULL,
    name_en varchar(255) NULL,
    full_name varchar(255) NULL,
    full_name_en varchar(255) NULL,
    code_name varchar(255) NULL,
    province_code varchar(20) NULL,
    administrative_unit_id integer NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT districts_new_pkey PRIMARY KEY (code)
    );

CREATE TABLE IF NOT EXISTS wards_new (
                                         code varchar(20) NOT NULL,
    "name" varchar(255) NOT NULL,
    name_en varchar(255) NULL,
    full_name varchar(255) NULL,
    full_name_en varchar(255) NULL,
    code_name varchar(255) NULL,
    district_code varchar(20) NULL,
    administrative_unit_id integer NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT wards_new_pkey PRIMARY KEY (code)
    );

-- Modify addresses table
ALTER TABLE addresses
DROP CONSTRAINT IF EXISTS fk_addresses_province,
  DROP CONSTRAINT IF EXISTS fk_addresses_district,
  DROP CONSTRAINT IF EXISTS fk_addresses_ward;

-- Add foreign keys to new tables
ALTER TABLE provinces_new
    ADD CONSTRAINT provinces_administrative_region_id_fkey
        FOREIGN KEY (administrative_region_id) REFERENCES administrative_regions(id);

ALTER TABLE provinces_new
    ADD CONSTRAINT provinces_administrative_unit_id_fkey
        FOREIGN KEY (administrative_unit_id) REFERENCES administrative_units(id);

ALTER TABLE districts_new
    ADD CONSTRAINT districts_administrative_unit_id_fkey
        FOREIGN KEY (administrative_unit_id) REFERENCES administrative_units(id);

ALTER TABLE districts_new
    ADD CONSTRAINT districts_province_code_fkey
        FOREIGN KEY (province_code) REFERENCES provinces_new(code);

ALTER TABLE wards_new
    ADD CONSTRAINT wards_administrative_unit_id_fkey
        FOREIGN KEY (administrative_unit_id) REFERENCES administrative_units(id);

ALTER TABLE wards_new
    ADD CONSTRAINT wards_district_code_fkey
        FOREIGN KEY (district_code) REFERENCES districts_new(code);

-- Drop original tables (since there's no data)
DROP TABLE IF EXISTS wards CASCADE;
DROP TABLE IF EXISTS districts CASCADE;
DROP TABLE IF EXISTS provinces CASCADE;

-- Rename new tables to original names
ALTER TABLE provinces_new RENAME TO provinces;
ALTER TABLE districts_new RENAME TO districts;
ALTER TABLE wards_new RENAME TO wards;

-- Update addresses table references
ALTER TABLE addresses
    ADD COLUMN province_code varchar(20) REFERENCES provinces(code),
  ADD COLUMN district_code varchar(20) REFERENCES districts(code),
  ADD COLUMN ward_code varchar(20) REFERENCES wards(code);

-- Create indexes
CREATE INDEX idx_provinces_region ON provinces(administrative_region_id);
CREATE INDEX idx_provinces_unit ON provinces(administrative_unit_id);
CREATE INDEX idx_districts_province ON districts(province_code);
CREATE INDEX idx_districts_unit ON districts(administrative_unit_id);
CREATE INDEX idx_wards_district ON wards(district_code);
CREATE INDEX idx_wards_unit ON wards(administrative_unit_id);