import sqlite3
import json

conn = sqlite3.connect('agropulse.db')
cursor = conn.cursor()

print("=== GREENHOUSES ===")
cursor.execute("SELECT * FROM greenhouses;")
for g in cursor.fetchall():
    print(g)

print("\n=== SENSORS ===")
cursor.execute("SELECT * FROM sensors;")
for s in cursor.fetchall():
    print(s)

print("\n=== CROPS ===")
cursor.execute("SELECT * FROM crops;")
for c in cursor.fetchall():
    print(c)

print("\n=== ACTUATORS ===")
cursor.execute("SELECT * FROM actuators;")
for a in cursor.fetchall():
    print(a)

conn.close()