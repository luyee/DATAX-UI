package net.iharding.modules.meta.controller;

import org.guess.core.web.BaseController;
import net.iharding.modules.meta.model.Owner;
import net.iharding.modules.meta.service.OwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
* 
* @ClassName: Owner
* @Description: OwnerController
* @author Joe.zhang
* @date  2016-5-18 14:17:31
*
*/
@Controller
@RequestMapping("/meta/owner")
public class OwnerController extends BaseController<Owner>{

	{
		editView = "/meta/owner/edit";
		listView = "/meta/owner/list";
		showView = "/meta/owner/show";
	}
	
	@Autowired
	private OwnerService ownerService;
}