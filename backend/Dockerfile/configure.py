from pymongo import MongoClient
from hashlib import md5

# Global variables
db_port = 27017
db_name = "mydb"
db_big_images_collection_name = "big_images"
db_users_collection_name = "users"

db_image_lifetime_days = 7

#client = MongoClient(host="104.155.30.208", port=db_port)
client = MongoClient(host="104.155.11.196", port=db_port)
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.120.99'}, {'_id': 1, 'host': '104.155.11.196'}, {'_id': 2, 'host': '130.211.89.136'}]}
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '130.211.89.136'}, {'_id': 1, 'host': '104.155.11.196'}, {'_id': 2, 'host': '104.155.120.99'}]}
config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.11.196'}, {'_id': 1, 'host': '104.155.120.99'}, {'_id': 2, 'host': '130.211.89.136'}]}
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.30.208'}]}
#client.admin.command('replSetInitiate', config)
db = client.get_database(db_name)

big_images_collection = db.get_collection(db_big_images_collection_name)
users_collection = db.get_collection(db_users_collection_name)

# automatically delete images with lifetime > db_image_lifetime_seconds
db_image_lifetime_seconds = 60 * 60 * 24 * db_image_lifetime_days
big_images_collection.ensure_index('dateTime', expireAfterSeconds=db_image_lifetime_seconds)


# User and passwords information
usr1 = 'foo'
# password: hello
pwd1 = '5d41402abc4b2a76b9719d911017c592'

usr2 = 'ivan'
# password: 12345
pwd2 = '827ccb0eea8a706c4c34a16891f84e7b'

usr3 = 'andreas'
# password: 12345
pwd3 = '827ccb0eea8a706c4c34a16891f84e7b'

usr4 = 'kylin'
# password: 12345
pwd4 = '827ccb0eea8a706c4c34a16891f84e7b'

users_collection.insert({'username': usr1, 'password': pwd1})
users_collection.insert({'username': usr2, 'password': pwd2})
users_collection.insert({'username': usr3, 'password': pwd3})
users_collection.insert({'username': usr4, 'password': pwd4})
