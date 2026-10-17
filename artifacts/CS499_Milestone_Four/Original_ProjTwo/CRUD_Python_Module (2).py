from pymongo import MongoClient

"""
AnimalShelter CRUD Module
Provides Create, Read, Update, and Delete functionality
for the AAC MongoDB database used in CS-340.
"""


class AnimalShelter:
    """CRUD operations for the animals collection in MongoDB."""

    def __init__(self, username: str, password: str):
        """
        Initialize the MongoDB connection using the provided credentials.
        The AAC database and animals collection are hard-coded for this project.
        """
        try:
            # Create MongoDB client connection using local host and admin auth source
            self.client = MongoClient(
                f"mongodb://{username}:{password}@localhost:27017/?authSource=admin"
            )

            # Store references to the target database and collection
            self.database = self.client["aac"]
            self.collection = self.database["animals"]

        except Exception as e:
            # Print a readable error if the database connection fails
            print("Connection error:", e)

    def create(self, data: dict) -> bool:
        """
        Insert one document into the collection.
        Returns True if the insert succeeds, otherwise returns False.
        """
        try:
            # Validate that the user supplied a dictionary for the insert
            if not isinstance(data, dict):
                raise ValueError("Data must be a dictionary.")

            # Insert the document and return whether MongoDB acknowledged it
            result = self.collection.insert_one(data)
            return result.acknowledged

        except Exception as e:
            # Return False if any error occurs during the insert
            print("Create error:", e)
            return False

    def read(self, query: dict = None) -> list:
        """
        Read matching documents from the collection.
        If no query is supplied, all documents are returned.
        """
        try:
            # Use an empty query to return every record in the collection
            if query is None:
                query = {}

            # MongoDB find() returns a cursor, so convert it to a list for Dash
            results = self.collection.find(query)
            return list(results)

        except Exception as e:
            # Return an empty list if the query fails
            print("Read error:", e)
            return []

    def update(self, query: dict, new_values: dict) -> int:
        """
        Update all documents that match the query using $set.
        Returns the number of modified documents.
        """
        try:
            # Validate both arguments before attempting the update
            if not isinstance(query, dict) or not isinstance(new_values, dict):
                raise ValueError("Query and new values must be dictionaries.")

            # update_many is used so the method can support multiple matching records
            result = self.collection.update_many(query, {"$set": new_values})
            return result.modified_count

        except Exception as e:
            # Return 0 if the update fails
            print("Update error:", e)
            return 0

    def delete(self, query: dict) -> int:
        """
        Delete all documents that match the query.
        Returns the number of deleted documents.
        """
        try:
            # Validate the query before attempting the delete
            if not isinstance(query, dict):
                raise ValueError("Query must be a dictionary.")

            # delete_many is used so the method can support multiple matching records
            result = self.collection.delete_many(query)
            return result.deleted_count

        except Exception as e:
            # Return 0 if the delete fails
            print("Delete error:", e)
            return 0