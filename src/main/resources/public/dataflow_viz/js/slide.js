/**
 * Created by zouxuan on 2/28/17.
 */

$(function(){
    $('.slider-arrow').click(function(){
        if($(this).hasClass('show')){
            $( ".slider-arrow, .slide_panel" ).animate({
                right: "+=500"
            }, 300, function() {
                // Animation complete.
            });
            $(this).html('&raquo;').removeClass('show').addClass('disappear');
        }
        else {
            $( ".slider-arrow, .slide_panel" ).animate({
                right: "-=500"
            }, 300, function() {
                // Animation complete.
            });
            $(this).html('&laquo;').removeClass('disappear').addClass('show');
        }
    });

});