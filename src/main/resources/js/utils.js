(function(exports){
   var utils = {}
   
   function dragMoveListener (event) {
       var target = event.target,
           // keep the dragged position in the data-x/data-y attributes
           x = (parseFloat(target.getAttribute('data-x')) || 0) + event.dx,
           y = (parseFloat(target.getAttribute('data-y')) || 0) + event.dy;

       // translate the element
       target.style.webkitTransform =
           target.style.transform =
           'translate(' + x + 'px, ' + y + 'px)';

       // update the posiion attributes
       target.setAttribute('data-x', x);
       target.setAttribute('data-y', y);
   }
   
   utils.draggable = function(id){
       interact('#'+id)
           .draggable({
           // enable inertial throwing
           inertia: true,
           // keep the element within the area of it's parent

           // enable autoScroll
           autoScroll: true,

           // call this function on every dragmove event
           onmove: dragMoveListener,
           // call this function on every dragend event
           onend: function (event) {
               var textEl = event.target.querySelector('p');

               textEl && (textEl.textContent =
                          'moved a distance of '
                          + (Math.sqrt(event.dx * event.dx +
                                       event.dy * event.dy)|0) + 'px');
           }
       });
   }   
   exports.utils = utils
   
})(this)