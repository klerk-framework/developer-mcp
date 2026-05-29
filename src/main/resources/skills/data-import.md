---
name: data-import
description: Import data into a Klerk application. 
---

# Data import
Sometimes you need to import data into a Klerk application. Don't import data directly into the database, as this
may lead to data inconsistencies.

## When to use
E.g. when migrating from a SQL database.

## How
The best way is to generate an API using klerk-graphql. Then you can write a script to import the data.
