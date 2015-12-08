"use struct";

function addX() {
    console.log("add X");
}

jQuery(document).ready(function () {
    console.log("document id ready");
    $('.diff-line').hover(
	function () {
	    var t = $(this)[0];
	    t.children[0].style.display = "block";
	},
	function () {
	    var t = $(this)[0];
	    t.children[0].style.display = "none";
	}
    );
});
