-- Create tables
CREATE TABLE "Users" (
    "id" uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    "username" varchar UNIQUE NOT NULL,
    "email" varchar NOT NULL,
    "firstName" varchar NOT NULL,
    "lastName" varchar NOT NULL
);

CREATE TABLE "RasterDefinitions" (
    "id" uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    "userId" uuid NOT NULL,
    "name" varchar NOT NULL,
    "outputGranuleExtentFlag" boolean NOT NULL,
    "outputSamplingGridType" varchar NOT NULL,
    "rasterResolution" int NOT NULL,
    "utmZoneAdjust" int,
    "mgrsBandAdjust" int,
    FOREIGN KEY ("userId") REFERENCES "Users" ("id")
);

CREATE TABLE "L2RasterProducts" (
    "id" uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    "timestamp" timestamp with time zone NOT NULL DEFAULT current_timestamp,
    "cycle" int NOT NULL,
    "pass" int NOT NULL,
    "scene" int NOT NULL,
    "outputGranuleExtentFlag" boolean NOT NULL,
    "outputSamplingGridType" varchar NOT NULL,
    "rasterResolution" int NOT NULL,
    "utmZoneAdjust" int,
    "mgrsBandAdjust" int
);

CREATE TABLE "Granules" (
    "id" uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    "productId" uuid NOT NULL,
    "timestamp" timestamp with time zone NOT NULL DEFAULT current_timestamp,
    "uri" varchar NOT NULL,
    FOREIGN KEY ("productId") REFERENCES "L2RasterProducts" ("id")
);

CREATE TABLE "ProductHistory" (
    "requestedById" uuid,
    "rasterProductId" uuid,
    "timestamp" timestamp with time zone NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY ("requestedById", "rasterProductId"),
    FOREIGN KEY ("requestedById") REFERENCES "Users" ("id"),
    FOREIGN KEY ("rasterProductId") REFERENCES "L2RasterProducts" ("id") ON DELETE CASCADE
);

CREATE TABLE "Status" (
    "id" uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    "productId" uuid NOT NULL,
    "timestamp" timestamp with time zone NOT NULL DEFAULT current_timestamp,
    "state" varchar NOT NULL,
    "reason" text,
    FOREIGN KEY ("productId") REFERENCES "L2RasterProducts" ("id") ON DELETE CASCADE
);
