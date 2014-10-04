window.list_children = (jnode) ->
    $ = window.$
    $.get 'drive',(data) ->
        for f in data.children
            jnode.append $("<p/>").append $("<a/>").attr('href', f.link).text(f.title)
