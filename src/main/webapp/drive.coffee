window.list_children = (jnode) ->
    $ = window.$
    fid = window.location.hash.slice(1)
    if fid == ''
        fid = 'root'
    $.post 'drive',JSON.stringify({folder_id:fid}),(data) ->
        origin = window.location.href
        last_hash = origin.lastIndexOf('#')
        if last_hash >= 0
            origin = origin.slice(0, last_hash)
        for f in data.folders
            jnode.append $("<p/>").append $("<a/>").attr('href', origin + '#' + f.id).text(f.title)
        for f in data.files
            jnode.append $("<p/>").append $("<a/>").attr('href', f.link).text(f.title)
