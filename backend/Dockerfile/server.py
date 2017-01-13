import builtins

original_open = open
def bin_open(filename, mode='rb'):       # note, the default mode now opens in binary
    return original_open(filename, mode)

from PIL import Image
import pytesseract
import bottle
from bottle import request, run, post, get, route
import base64
from io import BytesIO
import json
from pymongo import MongoClient
import datetime

# Global variables needed to manage the connections
address = "0.0.0.0"
server_port = "8080"

db_port = 27017
db_name = "mydb"
db_images_collection_name = "images"
db_big_images_collection_name = "big_images"
db_users_collection_name = "users"

thumbnail_image_boundaries = (300, 300)

bottle.BaseRequest.MEMFILE_MAX = 1024 * 1024 * 20

# Database related stuff
client = MongoClient(host="104.155.11.196", port=db_port)
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.120.99'}, {'_id': 1, 'host': ' 104.155.11.196'}, {'_id': 2, 'host': '130.211.89.136'}]}
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '130.211.89.136'}, {'_id': 1, 'host': '104.155.11.196'}, {'_id': 2, 'host': '104.155.120.99'}]}
config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.11.196'}, {'_id': 1, 'host': '104.155.120.99'}, {'_id': 2, 'host': '130.211.89.136'}]}
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.30.208'}]}
#client = MongoClient(host="104.155.30.208", port=db_port)
#client = MongoClient(host="104.155.30.208", port=db_port)
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.11.196'}, {'_id': 1, 'host': '104.155.120.99'}, {'_id': 2, 'host': '130.211.89.136'}]}
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.30.208'}, {'_id': 1, 'host': '104.199.99.93'}, {'_id': 2, 'host': '104.199.101.150'}]}
#config = {'_id': 'my_replica_set', 'members': [{'_id': 0, 'host': '104.155.30.208'}]}
#client.admin.command('replSetInitiate', config)
db = client.get_database(db_name)
images_collection = db.get_collection(db_images_collection_name)
big_images_collection = db.get_collection(db_big_images_collection_name)
users_collection = db.get_collection(db_users_collection_name)


# BottlePy related stuff

@get('/')
def ind():
    print("Hello!")

@post('/recognize_text')
def recognize_text():
    result_json_str = ""

    img_width = request.json['imgWidth']
    img_height = request.json['imgHeight']
    usr = request.json['username']

    img_decoded = base64.standard_b64decode(request.json['imgEncoded'])
    bio = BytesIO(img_decoded)
    img = Image.open(bio)
    thumbnail_img = img.copy()
    thumbnail_img.thumbnail(thumbnail_image_boundaries)
    print(img.size, thumbnail_img.size)
    print(img_width, img_height)
    encoded_thumbnail_img = img_to_base64_str(thumbnail_img)

    db_res = images_collection.find({'imgThumbnailEncoded': encoded_thumbnail_img, 'username': usr})

    if db_res.count() > 0:
        print("Found image in the database for current user!")
        # There is such image in the database!
        sample = db_res.next()
        # print(sample)
        data_to_send = {'recognizedText': sample['recognizedText']}
        print("Recognized text: " + sample['recognizedText'])
        result_json_str = json.dumps(data_to_send)
    else:
        print("Didn't find image in the database for current user!")
        # There is no such image in the database
        recognized_text = ""
        try:
            builtins.open = bin_open
            recognized_text = pytesseract.image_to_string(img).decode("utf8", "ignore")
        finally:
            builtins.open = original_open
        
        # print(recognized_text)

        data_to_send = {'recognizedText': recognized_text}
        result_json_str = json.dumps(data_to_send)

        id = big_images_collection.insert({'imgEncoded': request.json['imgEncoded'],
                                           'dateTime': datetime.datetime.utcnow()})

        # Add image stuff to the database
        data_to_db = {'imgThumbnailEncoded': encoded_thumbnail_img, 'imgWidth': img_width,
                      'imgHeight': img_height, 'recognizedText': recognized_text,
                      'dateTime': datetime.datetime.utcnow(), 'idToBigImg': id,
                      'username': usr}
        images_collection.insert(data_to_db)

    return result_json_str


@route('/history/<username>')
def history(username):
    print(username)
    db_res = images_collection.find({'username': username})
    res = []
    for doc in db_res:
        data = {'imgThumbnailEncoded': doc['imgThumbnailEncoded'].decode('utf-8'), 'recognizedText':  doc['recognizedText']}
        res.append(data)

    result = {'data': res}
    return json.dumps(result)


@post('/details')
def image_details():
    res = {}
    encoded_thumbnail_img = request.json['imgThumbnailEncoded']
    db_res = images_collection.find({'imgThumbnailEncoded': encoded_thumbnail_img.encode(),
                                     'username': request.json['username']})

    if db_res.count() > 0:
        sample = db_res.next()
        id = sample['idToBigImg']
        text = sample['recognizedText']

        res['recognizedText'] = text

        db_big_img_res = big_images_collection.find({'_id': id})
        if db_big_img_res.count() > 0:
            # There is a big instance of our image
            sample_big_img = db_big_img_res.next()
            img_encoded = sample_big_img['imgEncoded']
            res['imgEncoded'] = img_encoded
        else:
            # There is not big instance image in the database
            # In this case we'll use just thumbnail image
            res['imgEncoded'] = encoded_thumbnail_img
    else:
        # Really weird case
        print("Can't find such image in the database!")

    return json.dumps(res)


@post('/auth')
def auth():
    res = {'ok': 0}

    username = request.json['username']
    md5_pswd = request.json['password']

    db_res = users_collection.find({'username': username})
    if db_res.count() > 0:
        # There is such user with username = username
        sample = db_res.next()
        print(md5_pswd)
        print(sample['password'])

        if md5_pswd == sample['password']:
            res['ok'] = 1

    return json.dumps(res)


def img_to_base64_str(img):
    buffer = BytesIO()
    img.save(buffer, format="JPEG")
    img_str = base64.b64encode(buffer.getvalue())
    return img_str


run(host=address, port=server_port)
