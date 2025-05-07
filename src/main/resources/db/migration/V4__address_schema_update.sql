-- Drop existing foreign keys
ALTER TABLE IF EXISTS addresses DROP CONSTRAINT IF EXISTS fk_addresses_province;
ALTER TABLE IF EXISTS addresses DROP CONSTRAINT IF EXISTS fk_addresses_district;
ALTER TABLE IF EXISTS addresses DROP CONSTRAINT IF EXISTS fk_addresses_ward;
ALTER TABLE IF EXISTS districts DROP CONSTRAINT IF EXISTS fk_districts_province;
ALTER TABLE IF EXISTS wards DROP CONSTRAINT IF EXISTS fk_districts_ward;

-- Create administrative_regions table
CREATE TABLE administrative_regions (
                                        id integer NOT NULL,
                                        "name" varchar(255) NOT NULL,
                                        name_en varchar(255) NOT NULL,
                                        code_name varchar(255) NULL,
                                        code_name_en varchar(255) NULL,
                                        CONSTRAINT administrative_regions_pkey PRIMARY KEY (id)
);

-- Create administrative_units table
CREATE TABLE administrative_units (
                                      id integer NOT NULL,
                                      full_name varchar(255) NULL,
                                      full_name_en varchar(255) NULL,
                                      short_name varchar(255) NULL,
                                      short_name_en varchar(255) NULL,
                                      code_name varchar(255) NULL,
                                      code_name_en varchar(255) NULL,
                                      CONSTRAINT administrative_units_pkey PRIMARY KEY (id)
);

-- Drop and recreate provinces table
DROP TABLE IF EXISTS provinces CASCADE;
CREATE TABLE provinces (
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
                           CONSTRAINT provinces_pkey PRIMARY KEY (code)
);

-- Drop and recreate districts table
DROP TABLE IF EXISTS districts CASCADE;
CREATE TABLE districts (
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
                           CONSTRAINT districts_pkey PRIMARY KEY (code)
);

-- Drop and recreate wards table
DROP TABLE IF EXISTS wards CASCADE;
CREATE TABLE wards (
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
                       CONSTRAINT wards_pkey PRIMARY KEY (code)
);

-- Add foreign keys
ALTER TABLE provinces ADD CONSTRAINT provinces_administrative_region_id_fkey
    FOREIGN KEY (administrative_region_id) REFERENCES administrative_regions(id);
ALTER TABLE provinces ADD CONSTRAINT provinces_administrative_unit_id_fkey
    FOREIGN KEY (administrative_unit_id) REFERENCES administrative_units(id);

ALTER TABLE districts ADD CONSTRAINT districts_administrative_unit_id_fkey
    FOREIGN KEY (administrative_unit_id) REFERENCES administrative_units(id);
ALTER TABLE districts ADD CONSTRAINT districts_province_code_fkey
    FOREIGN KEY (province_code) REFERENCES provinces(code);

ALTER TABLE wards ADD CONSTRAINT wards_administrative_unit_id_fkey
    FOREIGN KEY (administrative_unit_id) REFERENCES administrative_units(id);
ALTER TABLE wards ADD CONSTRAINT wards_district_code_fkey
    FOREIGN KEY (district_code) REFERENCES districts(code);

-- Update addresses table references
ALTER TABLE addresses DROP COLUMN IF EXISTS province_code;
ALTER TABLE addresses DROP COLUMN IF EXISTS district_code;
ALTER TABLE addresses DROP COLUMN IF EXISTS ward_code;

ALTER TABLE addresses ADD COLUMN province_code varchar(20) REFERENCES provinces(code);
ALTER TABLE addresses ADD COLUMN district_code varchar(20) REFERENCES districts(code);
ALTER TABLE addresses ADD COLUMN ward_code varchar(20) REFERENCES wards(code);

-- Create indexes
CREATE INDEX idx_provinces_region ON provinces(administrative_region_id);
CREATE INDEX idx_provinces_unit ON provinces(administrative_unit_id);
CREATE INDEX idx_districts_province ON districts(province_code);
CREATE INDEX idx_districts_unit ON districts(administrative_unit_id);
CREATE INDEX idx_wards_district ON wards(district_code);
CREATE INDEX idx_wards_unit ON wards(administrative_unit_id);