""" Chinese text classifier
@ref https://ckip-transformers.readthedocs.io/en/stable/main/readme.html#git
"""

from ckip_transformers.nlp import (
    CkipWordSegmenter, 
    CkipPosTagger, 
    CkipNerChunker
)


class ChineseTextClassifier:

    def __init__(self):

        self.ws_driver  = CkipWordSegmenter(model="bert-base")
        self.pos_driver = CkipPosTagger(model="bert-base")
        self.ner_driver = CkipNerChunker(model="bert-base")

    def run_single_word_segmentation(self, text):
        assert(isinstance(text, list) == True)
        ws  = self.ws_driver(text, use_delim=True)
        return ws

    def run_pipeline(self, text):
        assert(isinstance(text, list) == True)
        ws  = self.ws_driver(text, use_delim=True)
        pos = self.pos_driver(ws, use_delim=True)
        ner = self.ner_driver(text, use_delim=True)
        print(ws)
        print(pos)
        print(ner)
        return self._prepare_results(ws, pos, ner)

    def _prepare_results(self, ws, pos, ner):
        res = []
        for sentence_ws, sentence_pos, sentence_ner in zip(ws, pos, ner):
            packed = self._combine_results(sentence_ws, sentence_pos)
            res.append(packed)
        return "\u3000".join(res)

    def _combine_results(self, sentence_ws, sentence_pos):
        assert len(sentence_ws) == len(sentence_pos)
        res = []
        for word_ws, word_pos in zip(sentence_ws, sentence_pos):
            res.append(f"{word_ws}({word_pos})")
        return "\u3000".join(res)

# test script block
model = ChineseTextClassifier()
# classified = model.run_single_word_segmentation(["之后你看看了我的出版请告诉我你认为什么"])
# print(classified)

