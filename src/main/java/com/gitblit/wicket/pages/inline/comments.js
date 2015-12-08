"use struct";

function addX() {
    console.log("add X");
}

jQuery(document).ready(function () {
    console.log("document id ready");
    addX();
});