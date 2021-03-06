




function getContainsTextPath(element){

    if(!childHasText(element)){

            var content = element.innerText;
            if(content){
                content = content.trim();
                    if(content.length > 0){

                        var xpath = '//*[text()[contains(.,"' + content + '")]]';

                        xpath = specify(xpath, element);

                        return xpath;
                    }
            }
    }
    return '';
}

function specify(xpath, element){

    var found = document.evaluate('count(' + root + xpathPart + ')', document, null, XPathResult.ANY_TYPE, null);
    if(found.numberValue > 2) {



    }

    return xpath;

}




function childHasText(element){

  var children = element.childNodes;

  var hasText = false;

  for (var i = 0; i < children.length; i++) {

    var content = children[i].innerText;
    if(content){
        content = content.trim();
            if(content.length > 0){
                hasText = true;
            }
    }


  }


  return hasText;


}



function getTagNameOnlyPath(element) {

  try {

    if (element.tagName == 'BODY') {
      return 'BODY';
    }

    var path = getTagNameOnlyPath(element.parentNode);

    return path + ' ' + element.tagName + getPreviousSiblings(element);
  } catch (error) {

  }
}

function getPreviousSiblings(element) {

  var siblings = [];
  var type = element.tagName;
  while (element = element.previousSibling) {
    if (element.tagName == type) {
      siblings.push(element);
    }
  }

  var cssString = '';
  cssString += ':nth-of-type(' + (siblings.length + 1) + ')';
  return cssString;
}


var elementOfInterest;

function getTextContainedPath(element) {

  console.log('in getTextContainedPath');


  try {

    if (!elementOfInterest) {
      elementOfInterest = element;
    }

    if (element.tagName == 'BODY') {
      return '/';
    }

    var root = '/';
    var xpathPart = getXpathPart(element);

    //var found = document.evaluate('count(' + root + xpathPart + ')', document, null, XPathResult.ANY_TYPE, null);

      path = root;
      if (true){//found.numberValue > 2) {
        path = getTextContainedPath(element.parentNode);
      }

      return path + xpathPart;

    } catch (error) {
        console.log(error);

    }
  }

  function getXpathPart(element) {

    var textContained = getTextContained(element);

    if (textContained.length > 0) {

      return '/*[text()[contains(.,"' + textContained + '")]]';
    } else {
      return '/*';
    }
  }

  function getTextContained(element) {

  console.log('in getTextContained');



    if (elementOfInterest === element) {

      return chooseBestSubstring(element.innerText);

    } else {

      child = element.firstChild;
        texts = [];

      while (child) {
        if (child.nodeType == 3) {
          newText = child.data;
          trimmed = newText.trim();
          if(trimmed.length > 0){
              texts.push(trimmed);
          }
        }
        child = child.nextSibling;
      }

      var textString = texts.join('\\');

        return textString;

      }

    }

    function chooseBestSubstring(parentString) {

      console.log('in chooseBestSubstring');


      var subs = parentString.split('\\n');
      var longSub = '';
      for (var i = 0; i < subs.length; i++) {
        if (subs[i].length > longSub.length) {
          longSub = subs[i];
        }
      }

      var choice = longSub.substr(0, 47);
      console.log('choice: ' + choice);
      return choice;
    }

var backupLocators = new Object();
backupLocators['nth-of-type'] = getTagNameOnlyPath(arguments[0]);

return JSON.stringify(backupLocators);

