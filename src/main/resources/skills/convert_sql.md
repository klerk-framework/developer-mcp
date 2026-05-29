---
name: convert-from-sql
description: Create Klerk models by inspecting a SQL database.
---

# Convert from SQL
Can generate Klerk models from an existing SQL database (mariadb, mysql). It creates simple "Create, Update, Delete" models, which can be
a good starting point for a Klerk project.

## When to use
If the task is to convert an existing SQL-based application to Klerk, this skill can be used. Typically only used in the beginning of a project.

## How to use
Connect to the database and find the relevant tables in the database. You may have to ask if all tables should be converted or only some.

For each table, execute this query (replace <database> and <table name>):
```sql
SELECT
c.TABLE_NAME,
c.COLUMN_NAME,
c.DATA_TYPE,
c.IS_NULLABLE,
c.COLUMN_DEFAULT,
c.CHARACTER_MAXIMUM_LENGTH,
rc.REFERENCED_TABLE_NAME
FROM information_schema.COLUMNS c
LEFT JOIN information_schema.KEY_COLUMN_USAGE kcu
ON  kcu.TABLE_SCHEMA  = c.TABLE_SCHEMA
AND kcu.TABLE_NAME    = c.TABLE_NAME
AND kcu.COLUMN_NAME   = c.COLUMN_NAME
AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
LEFT JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
ON  rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
AND rc.CONSTRAINT_NAME   = kcu.CONSTRAINT_NAME
AND rc.TABLE_NAME        = kcu.TABLE_NAME
WHERE c.TABLE_SCHEMA = '<database>' and c.TABLE_NAME = '<table name>'
ORDER BY c.ORDINAL_POSITION;
```

Create a Klerk model using the `generate_model` tool. The tool takes the following arguments:
* model_name: use TABLE_NAME as the model name (PascalCase)
* properties: a list of properties to create. Each property is a comma-separated list of the following:
  * name: COLUMN_NAME (camelCase)
  * type: see below how to convert a SQL type to a Klerk type
  * nullable: IS_NULLABLE
  * default_value: COLUMN_DEFAULT
  * model_reference: use REFERENCED_TABLE_NAME (PascalCase) or null if the type is not a reference to another model

There may be references to models that have not been created yet, so don't check that the code compiles until you have created all models.

Repeat until all tables have been converted.

Note that when you are done, there will only be basic "Create, Update, Delete" models. As the next step you probably should
ask the developer if you should proceed and examine the old applicaiton code to extract more information and update
the klerk models, validation rules etc. 

## Converting SQL types to Klerk types

## Special cases
Created_at/updated_at columns should be ignored as Klerk provides the corresponding functionality.

Soft-delete columns should be ignored, and you should instead first proceed and create the model. When the model exists,
you can model the soft-delete functionality by adding a "deleted" state in the model's state machine.

## Data migration
Data migration is not something that this MCP can 
