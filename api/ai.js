export default async function handler(req,res){
  if(req.method!=="POST") return res.status(405).json({error:"Method not allowed"});
  const auth=req.headers.authorization||"";
  const token=auth.startsWith("Bearer ")?auth.slice(7):"";
  if(!token) return res.status(401).json({error:"Missing session token"});

  const supabaseUrl=process.env.SUPABASE_URL||"https://cjsiwwbqbmviucpezglt.supabase.co";
  const supabaseKey=process.env.SUPABASE_PUBLISHABLE_KEY||"";
  if(!supabaseKey) return res.status(500).json({error:"SUPABASE_PUBLISHABLE_KEY is not configured"});

  const userRes=await fetch(supabaseUrl+"/auth/v1/user",{
    headers:{apikey:supabaseKey,Authorization:"Bearer "+token}
  });
  if(!userRes.ok) return res.status(401).json({error:"Invalid MULTIWORK session"});
  const user=await userRes.json();

  const body=req.body||{};
  const message=String(body.message||"").slice(0,12000);
  const context=JSON.stringify(body.context||{}).slice(0,45000);
  if(!process.env.OPENAI_API_KEY) return res.status(503).json({error:"OpenAI is not configured yet. Add OPENAI_API_KEY to the Vercel environment variables."});

  const response=await fetch("https://api.openai.com/v1/responses",{
    method:"POST",
    headers:{
      "Content-Type":"application/json",
      Authorization:"Bearer "+process.env.OPENAI_API_KEY
    },
    body:JSON.stringify({
      model:process.env.OPENAI_MODEL||"gpt-6-astra",
      instructions:"You are MULTIWORK AI, an executive operating system. Use only the supplied workspace context for personal facts. Be concise, practical and action-oriented. Never invent meetings, people, emails, tasks or notes. If context is missing, say so and tell the user what connection or permission is needed. Do not execute sensitive actions from this endpoint; return a proposed action for the app to confirm.",
      input:[
        {role:"user",content:[{type:"input_text",text:"Workspace context:\n"+context+"\n\nUser request:\n"+message}]}
      ]
    })
  });
  const data=await response.json();
  if(!response.ok) return res.status(response.status).json({error:data?.error?.message||"OpenAI request failed"});
  return res.status(200).json({output:data.output_text||"No response generated.",user_id:user.id});
}
