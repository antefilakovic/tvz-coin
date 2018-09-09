function get (url, callback, failureCallback) {

    var xobj = new XMLHttpRequest();
    xobj.overrideMimeType("application/json");
    xobj.open('GET', url, true);

    xobj.onreadystatechange = function () {
        if (xobj.readyState == 4 && xobj.status == "200") {
            callback(xobj.responseText);
        } else if (xobj.status != "200") {
            failureCallback(xobj.status);
        }
    };
    xobj.send(null);
}

function blockName (index) {
    return index == 0 ? "Genesis" : "Block" + index;
}

function transactions (block) {
    return block.signedTransaction.map(function (m) {
        return "- " + m.transaction.hash.toString().substring(0, 7);
    })
}

function resolveNodes () {
    for (var i = 0; i < 2; i++) {
        var container = document.getElementById("blocks" + (i+1));

        if (container.children.length > 0) {
            for (var j = 0; j < container.children.length; j++) {
                container.removeChild(container.children[j]);
            }
        }
        loadOptions(i);
        fetchBlocks(i);
        fetchBalance(i);
        fetchAddress(i);
    }
}

function fetchBalance (index) {
    var port = 8080 + index;
    var url = "http://localhost:" + port + "/v1/balance/";

    var id = index + 1;
    var container = document.getElementById("balance" + id);

    get(url, function (response) {
        var balanceHolder = document.createElement("DIV");
        balanceHolder.appendChild(document.createElement("SPAN").appendChild(document.createTextNode("Balance:" + response)));

        if (container.children.length === 0) {
            container.appendChild(balanceHolder)
        } else {
            container.replaceChild(balanceHolder, container.children[0])
        }
    }, function (status) {
        var errorEl = document.createElement("DIV");
        errorEl.appendChild(document.createElement("SPAN").appendChild(document.createTextNode("Failed to fetch balance.")));
        if (container.children.length === 0) {
            container.appendChild(errorEl)
        } else {
            container.replaceChild(errorEl, container.children[0])
        }
    })
}

function fetchAddress (index) {
    var port = 8080 + index;
    var url = "http://localhost:" + port + "/v1/address/";

    var id = index + 1;
    var container = document.getElementById("address" + id);

    get(url, function (response) {
        container.innerText = response.substring(1, response.length - 1)
    }, function (status) {
    })
}

function loadOptions (index) {
    var select = document.getElementById("payee" + index);

    if (select.children.length === 1) {
        return;
    }

    for (var i = 0; i < select.children.length; i++) {
        select.removeChild(select.children[i]);
    }

    var addressByNode = {};

    addressByNode[0] = document.getElementById("address1").innerText;
    addressByNode[1] = document.getElementById("address2").innerText;
    // addressByNode[2] = document.getElementById("address3").innerText;

    // var array = index === 0 ? [1, 2] : (index === 1 ? [0, 2] : [0, 1]);
    var array = index === 0 ? [1] : [0];

    for (var j = 0; j < array.length; j++) {
        var option = document.createElement("option");
        var value = addressByNode[array[j]];
        if (value.length === 0) {
            break;
        }
        option.value = addressByNode[array[j]];
        option.text = "Node" + (array[j] + 1);

        select.appendChild(option);
    }
}

function addInfoText (container, block) {
    var blockHash = block.hash.substring(0, 7);
    var prevHash = block.previousHash.substring(0, 7);
    var nonce = block.nonce;

    var blockHashHolder = document.createElement("P");
    var prevHashHolder = document.createElement("P");
    var nonceHolder = document.createElement("P");

    var blockHashText = document.createTextNode("Hash: " + blockHash);
    var prevHashText = document.createTextNode("Previous hash: " + prevHash);
    var nonceText = document.createTextNode("Nonce: " + nonce);

    blockHashHolder.appendChild(blockHashText);
    prevHashHolder.appendChild(prevHashText);
    nonceHolder.appendChild(nonceText);

    container.appendChild(blockHashHolder);
    container.appendChild(prevHashHolder);
    container.appendChild(nonceHolder);

    //transactions
    var transactionsList = transactions(block);
    var transactionsHolder = document.createElement("DIV");

    var transactionsHeader = document.createElement("P");
    var transactionsHeaderText = document.createTextNode("Transactions:");
    transactionsHeader.appendChild(transactionsHeaderText);
    transactionsHolder.appendChild(transactionsHeader);

    for (var i = 0; i < transactionsList.length; i++) {
        var transactionEl = document.createElement("P");
        var transactionText = document.createTextNode(transactionsList[i]);
        transactionEl.appendChild(transactionText);
        transactionsHolder.appendChild(transactionEl);
    }

    container.appendChild(transactionsHolder);
}

function transfer (index) {
    event.preventDefault();

    var port = 8080 + index;
    var payeeSelect = document.getElementById("payee" + index);
    var address = payeeSelect.options[payeeSelect.selectedIndex].value;
    var amount = document.getElementById("amount" + index).value;

    var url = "http://localhost:" + port + "/v1/transaction/" + address + "/" + amount;

    get(url, function (response) {
        console.log("Sucessful transaction:" + response);
    }, function (status) {
        console.log("Failed transaction.")
    });

    return false;
}

function fetchBlocks (index) {

    var port = 8080 + index;
    var url = "http://localhost:" + port + "/v1/blocks/";

    var id = index + 1;
    var container = document.getElementById("blocks" + id);

    get(url, function (response) {
        var blocks = JSON.parse(response);
        console.log("Got " + blocks.length + " blocks for node " + id);

        // for (var i = 0; i < blocks.length; i++) {
        var i = blocks.length - 1;
            var block = blocks[i];

            var element = document.createElement("DIV");

            var blockNameEl = document.createElement("H3");
            var blockNameText = document.createTextNode(blockName(i));
            blockNameEl.appendChild(blockNameText);
            element.appendChild(blockNameEl);
            addInfoText(element, block);

            container.appendChild(element);
        // }

    }, function (status) {
        var errorEl = document.createElement("DIV");
        errorEl.appendChild(document.createElement("SPAN").appendChild(document.createTextNode("Failed to fetch blockchain.")));
        if (container.children.length === 0) {
            container.appendChild(errorEl)
        } else {
            container.replaceChild(errorEl, container.children[0])
        }
    });
}


