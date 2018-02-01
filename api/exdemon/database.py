#!/usr/bin/env python
# -*- coding: utf-8 -*-

from flask_sqlalchemy import SQLAlchemy
from exdemon.application import app

db = SQLAlchemy(app)

class Schema(db.Model):
    __tablename__ = 'schema'

    id = db.Column('id', db.Integer, primary_key=True, autoincrement=True)
    name = db.Column('name', db.String(32), nullable=False)
    project = db.Column('project', db.String(32), nullable=False)
    environment = db.Column('environment', db.String(32), nullable=False)
    data = db.Column('data', db.JSON, nullable=False)
    enabled = db.Column('enabled', db.Boolean, nullable=False)

    def __repr__(self):
        return "<Schema(id='%s', name='%s', project='%s', environment='%s', 'data='%s')>" % (
                        self.id, self.name, self.project, self.environment, self.data)

class Metric(db.Model):
    __tablename__ = 'metric'

    id = db.Column('id', db.Integer, primary_key=True, autoincrement=True)
    name = db.Column('name', db.String(32), nullable=False)
    project = db.Column('project', db.String(32), nullable=False)
    environment = db.Column('environment', db.String(32), nullable=False)
    data = db.Column('data', db.JSON, nullable=False)
    enabled = db.Column('enabled', db.Boolean, nullable=False)

    def __repr__(self):
        return "<Metric(id='%s', name='%s', project='%s', environment='%s', 'data='%s')>" % (
                        self.id, self.name, self.project, self.environment, self.data)

class Monitor(db.Model):
    __tablename__ = 'monitor'

    id = db.Column('id', db.Integer, primary_key=True, autoincrement=True)
    name = db.Column('name', db.String(32), nullable=False)
    project = db.Column('project', db.String(32), nullable=False)
    environment = db.Column('environment', db.String(32), nullable=False)
    data = db.Column('data', db.JSON, nullable=False)
    enabled = db.Column('enabled', db.Boolean, nullable=False)

    def __repr__(self):
        return "<Monitor(id='%s', name='%s', project='%s', environment='%s', 'data='%s')>" % (
                        self.id, self.name, self.project, self.environment, self.data)

if __name__ == "__main__":
    # Create the database schema
    db.create_all()
