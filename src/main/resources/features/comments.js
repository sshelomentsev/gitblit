"use struct";

function addX() {
    console.log("add X");
}

jQuery(document).ready(function () {
    console.log("document id ready");
	/*
	$(function() {
		$( "#dialog" ).dialog();
	});
	*/

	$('.inline-comment').click(function (e) {
		var target = e.target;
		var th = target.parentElement;
		var tr = th.parentElement;
		$("<tr id='commentTr'><td colspan='7'>" +
			"<a href=# class=comments-link>add a comment</a>" +
			"<div class='comment-form-container dno'>" +
			"<form>" +
			"<textarea name=comment></textarea>" +
			"<input type=submit>" +
			"</form></div></td></tr>").insertAfter(tr);
	});

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
