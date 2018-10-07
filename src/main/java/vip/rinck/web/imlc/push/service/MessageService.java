package vip.rinck.web.imlc.push.service;

import vip.rinck.web.imlc.push.bean.api.base.ResponseModel;
import vip.rinck.web.imlc.push.bean.api.message.MessageCreateModel;
import vip.rinck.web.imlc.push.bean.card.MessageCard;
import vip.rinck.web.imlc.push.bean.db.Group;
import vip.rinck.web.imlc.push.bean.db.Message;
import vip.rinck.web.imlc.push.bean.db.User;
import vip.rinck.web.imlc.push.factory.GroupFactory;
import vip.rinck.web.imlc.push.factory.MessageFactory;
import vip.rinck.web.imlc.push.factory.PushFactory;
import vip.rinck.web.imlc.push.factory.UserFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;


@Path("/msg")
public class MessageService extends BaseService{
    //发送一条消息到服务器
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<MessageCard> pushMessage(MessageCreateModel model) {
        if(!MessageCreateModel.check(model)){
            return ResponseModel.buildParameterError();
        }

        User self = getSelf();

        //查询数据库中是否存在
        Message message = MessageFactory.findById(model.getId());

        if(message!=null){
            //正常返回
            return ResponseModel.buildOk(new MessageCard(message));
        }

        if(model.getReceiverType()==Message.RECEIVER_TYPE_GROUP){
            return pushToGroup(self,model);
        }else{
            return pushToUser(self,model);
        }
    }

    //发送到用户
    private ResponseModel<MessageCard> pushToUser(User sender,MessageCreateModel model){
        User receiver = UserFactory.findById(model.getReceiverId());
        if(receiver==null)
            return ResponseModel.buildNotFoundUserError("Can't find receiver user");
        if(receiver.getId().equals(sender.getId())){
            //发送给自己 则返回创建消息失败
            return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);
        }
        //存储数据库
        Message message = MessageFactory.add(sender,receiver,model);

        return buildAndPushResponse(sender,message);
    }

    //发送到群
    private ResponseModel<MessageCard> pushToGroup(User sender,MessageCreateModel model){
        //找群是有权限性质的
        Group group = GroupFactory.findById(sender,model.getReceiverId());
        if(group==null){
            //没找到群 可能因为不是群成员
            return ResponseModel.buildNotFoundUserError("Can't find receiver group");
        }

        //添加到数据库
        Message message = MessageFactory.add(sender,group,model);

        //走通用的推送逻辑
        return buildAndPushResponse(sender,message);
    }

    //推送并构建一个返回信息
    private ResponseModel<MessageCard> buildAndPushResponse(User sender, Message message) {

        if(message==null){
            //存储数据库失败
            return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);
        }

        //进行推送
        PushFactory.pushNewMessage(sender,message);

        //返回
        return ResponseModel.buildOk(new MessageCard(message));
    }
}
