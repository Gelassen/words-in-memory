from flask import Flask, request, jsonify
from model import ChineseTextClassifier

app = Flask(__name__)

@app.route("/")
def hello_world():
    return "<p>Hello, World!</p>"

@app.route('/classify', methods=['POST'])
def classify():
    input_json = request.get_json(force=True) 
    # force=True, above, is necessary if another developer 
    # forgot to set the MIME type to 'application/json'
    # print('data from client:', input_json)
    # dictToReturn = {'answer':42}
    # return jsonify(dictToReturn)
    model = ChineseTextClassifier()
    test = model.run_single_word_segmentation(input_json.text)
    return { "classified_text": test }

if __name__ == '__main__':
    app.run(debug=True)