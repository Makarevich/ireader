window.list_children = (jnode) ->
    $ = window.$
    $.get 'drive',(data) ->
        for ch in data.children
            jnode.append $("<p/>").text(ch)
