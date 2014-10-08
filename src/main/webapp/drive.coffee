$ = window.$

render_folders = (jnode, folders) ->
    jnode.empty()
    wrapper = $('<ul/>').addClass('side-nav').appendTo(jnode)
    for f in folders
        $('<a/>').attr('href', build_query_string({id:f.id})).text(f.title)
                 .appendTo($('<li/>').appendTo(wrapper))

render_files = (jnode, files) ->
    jnode.empty()
    for f in files
        $('<a/>').appendTo(jnode).addClass('alert-box small success')
                                 .attr('href', f.link).text(f.title)

window.list_children = (folders_node, files_node) ->
    $ = window.$
    fid = get_query_params().id ? 'root'
    $.post 'drive',JSON.stringify({folder_id:fid}),(data) ->
        render_folders(folders_node, data.folders)
        render_files(files_node, data.files)
